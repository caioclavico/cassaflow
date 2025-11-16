(ns cassaflow.benchmark-insert-select
  "Benchmark comparing CassaFlow vs Alia for INSERT and SELECT operations separately.
   
   Usage: lein run -m cassaflow.benchmark-insert-select"
  (:require
   [cassaflow.core :as cflow]
   [cassaflow.client :as cflow-client]
   [qbits.alia :as alia])
  (:import
   [org.testcontainers.containers CassandraContainer]))

;; ============================================================================
;; Setup
;; ============================================================================

(defn setup-cassandra []
  (println "\n🚀 Starting Cassandra container...")
  (let [container (doto (CassandraContainer. "cassandra:4.1")
                    (.start))
        host (.getHost container)
        port (.getFirstMappedPort container)
        cflow-session (cflow-client/connect {:host host :port port})
        alia-cluster (alia/cluster {:contact-points [host]
                                    :port port})
        alia-session (alia/connect alia-cluster)]

    (println "📝 Creating schema...")
    (cflow/execute cflow-session
                   "CREATE KEYSPACE IF NOT EXISTS bench_ks WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}")

    (println "✅ Setup complete!\n")
    {:container container
     :cflow-session cflow-session
     :alia-cluster alia-cluster
     :alia-session alia-session}))

(defn teardown-cassandra [{:keys [container cflow-session alia-cluster]}]
  (println "\n🧹 Cleaning up...")
  (.close cflow-session)
  (alia/shutdown alia-cluster)
  (.stop container)
  (println "✅ Cleanup complete!"))

;; ============================================================================
;; Benchmark Functions
;; ============================================================================

(defn benchmark-inserts
  "Benchmarks INSERT operations"
  [n {:keys [cflow-session alia-session]}]
  (println (format "\n  📝 Testing %d INSERTs..." n))

  ;; Create fresh table for this test (avoiding tombstones)
  (let [table-name (str "users_insert_" n)]
    (cflow/execute cflow-session
                   (format "DROP TABLE IF EXISTS bench_ks.%s" table-name))
    (cflow/execute cflow-session
                   (format "CREATE TABLE bench_ks.%s (id text PRIMARY KEY, name text, age int, email text)" table-name))

    ;; Warm up
    (cflow/execute cflow-session
                   (format "INSERT INTO bench_ks.%s (id, name, age, email) VALUES (:id, :name, :age, :email)" table-name)
                   {:id "warmup" :name "Warmup" :age (Integer/valueOf 25) :email "warmup@test.com"})
    (alia/execute alia-session
                  (format "INSERT INTO bench_ks.%s (id, name, age, email) VALUES (?, ?, ?, ?)" table-name)
                  {:values ["warmup2" "Warmup2" (int 25) "warmup2@test.com"]})

    (cflow/execute cflow-session (format "TRUNCATE bench_ks.%s" table-name))

    ;; Benchmark CassaFlow INSERTs
    (print "    🔵 CassaFlow INSERT... ")
    (flush)
    (let [start (System/nanoTime)
          _ (dotimes [i n]
              (cflow/execute cflow-session
                             (format "INSERT INTO bench_ks.%s (id, name, age, email) VALUES (:id, :name, :age, :email)" table-name)
                             {:id (str "user-" i)
                              :name (str "User " i)
                              :age (Integer/valueOf (+ 20 (mod i 50)))
                              :email (str "user" i "@example.com")}))
          end (System/nanoTime)
          cflow-time (/ (- end start) 1000.0)] ; Total in microseconds
      (println (format "%.2f µs/insert (total: %.2f ms)" (/ cflow-time n) (/ cflow-time 1000.0)))

      ;; Clean for Alia test
      (cflow/execute cflow-session (format "TRUNCATE bench_ks.%s" table-name))

      ;; Benchmark Alia INSERTs
      (print "    🟢 Alia INSERT... ")
      (flush)
      (let [start (System/nanoTime)
            _ (dotimes [i n]
                (alia/execute alia-session
                              (format "INSERT INTO bench_ks.%s (id, name, age, email) VALUES (?, ?, ?, ?)" table-name)
                              {:values [(str "user-" i)
                                        (str "User " i)
                                        (int (+ 20 (mod i 50)))
                                        (str "user" i "@example.com")]}))
            end (System/nanoTime)
            alia-time (/ (- end start) 1000.0)] ; Total in microseconds
        (println (format "%.2f µs/insert (total: %.2f ms)" (/ alia-time n) (/ alia-time 1000.0)))

        {:rows n
         :operation "INSERT"
         :cassaflow-us-per-op (/ cflow-time n)
         :alia-us-per-op (/ alia-time n)
         :cassaflow-total-us cflow-time
         :alia-total-us alia-time
         :diff-percent (* 100 (/ (- cflow-time alia-time) alia-time))}))))

(defn benchmark-selects
  "Benchmarks SELECT operations with N rows already inserted"
  [n table-name {:keys [cflow-session alia-session]}]
  (println (format "\n  🔍 Testing SELECT with %d rows..." n))

  ;; Warm up
  (doall (cflow/execute cflow-session (format "SELECT * FROM bench_ks.%s" table-name)))
  (doall (alia/execute alia-session (format "SELECT * FROM bench_ks.%s" table-name)))

  ;; Benchmark CassaFlow SELECTs
  (print "    🔵 CassaFlow SELECT... ")
  (flush)
  (let [start (System/nanoTime)
        _ (dotimes [_ 60]
            (doall (cflow/execute cflow-session (format "SELECT * FROM bench_ks.%s" table-name))))
        end (System/nanoTime)
        cflow-time (/ (- end start) 60000.0)] ; Average in microseconds
    (println (format "%.2f µs" cflow-time))

    ;; Benchmark Alia SELECTs
    (print "    🟢 Alia SELECT... ")
    (flush)
    (let [start (System/nanoTime)
          _ (dotimes [_ 60]
              (doall (alia/execute alia-session (format "SELECT * FROM bench_ks.%s" table-name))))
          end (System/nanoTime)
          alia-time (/ (- end start) 60000.0)] ; Average in microseconds
      (println (format "%.2f µs" alia-time))

      {:rows n
       :operation "SELECT"
       :cassaflow-us cflow-time
       :alia-us alia-time
       :diff-percent (* 100 (/ (- cflow-time alia-time) alia-time))})))

;; ============================================================================
;; Main Benchmark
;; ============================================================================

(defn run-benchmark [env]
  (println "\n╔" (apply str (repeat 66 "=")) "╗")
  (println "║        📊 INSERT vs SELECT Benchmark Comparison       ║")
  (println "╚" (apply str (repeat 66 "=")) "╝")

  (let [row-counts [100 500 1000 5000 10000 50000 100000]
        ;; Run INSERT benchmarks and keep table names
        insert-results (mapv (fn [n]
                               (let [result (benchmark-inserts n env)
                                     table-name (str "users_insert_" n)]
                                 (assoc result :table-name table-name)))
                             row-counts)
        ;; Run SELECT benchmarks using the same tables from INSERTs
        select-results (mapv (fn [{:keys [rows table-name]}]
                               (benchmark-selects rows table-name env))
                             insert-results)]

    ;; INSERT Results
    (println "\n╔" (apply str (repeat 66 "=")) "╗")
    (println "║                  📝 INSERT Results                     ║")
    (println "╚" (apply str (repeat 66 "=")) "╝\n")
    (println (format "%-10s | %-20s | %-20s | %-10s"
                     "Rows" "CassaFlow (µs/op)" "Alia (µs/op)" "Diff %"))
    (println (apply str (repeat 75 "-")))
    (doseq [{:keys [rows cassaflow-us-per-op alia-us-per-op diff-percent]} insert-results]
      (let [winner (if (< cassaflow-us-per-op alia-us-per-op) "🏆" "  ")]
        (println (format "%-10d | %s%18.2f | %18.2f | %+9.1f%%"
                         rows winner cassaflow-us-per-op alia-us-per-op diff-percent))))

    ;; SELECT Results
    (println "\n╔" (apply str (repeat 66 "=")) "╗")
    (println "║                  🔍 SELECT Results                     ║")
    (println "╚" (apply str (repeat 66 "=")) "╝\n")
    (println (format "%-10s | %-20s | %-20s | %-10s"
                     "Rows" "CassaFlow (µs)" "Alia (µs)" "Diff %"))
    (println (apply str (repeat 75 "-")))
    (doseq [{:keys [rows cassaflow-us alia-us diff-percent]} select-results]
      (let [winner (if (< cassaflow-us alia-us) "🏆" "  ")]
        (println (format "%-10d | %s%18.2f | %18.2f | %+9.1f%%"
                         rows winner cassaflow-us alia-us diff-percent))))

    ;; Summary
    (println "\n╔" (apply str (repeat 66 "=")) "╗")
    (println "║                    📊 Summary                          ║")
    (println "╚" (apply str (repeat 66 "=")) "╝\n")
    (let [insert-wins (count (filter #(< (:cassaflow-us-per-op %) (:alia-us-per-op %)) insert-results))
          select-wins (count (filter #(< (:cassaflow-us %) (:alia-us %)) select-results))
          total (count row-counts)]
      (println (format "INSERT: CassaFlow won %d/%d tests (%.1f%%)"
                       insert-wins total (* 100.0 (/ insert-wins total))))
      (println (format "SELECT: CassaFlow won %d/%d tests (%.1f%%)"
                       select-wins total (* 100.0 (/ select-wins total)))))

    {:inserts insert-results
     :selects select-results}))

;; ============================================================================
;; Main
;; ============================================================================

(defn -main [& _args]
  (let [env (setup-cassandra)]
    (try
      (run-benchmark env)
      (finally
        (teardown-cassandra env))))
  (System/exit 0))