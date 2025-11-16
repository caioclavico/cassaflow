(ns cassaflow.core
  (:require
   [cassaflow.query :as q])
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [com.datastax.oss.driver.api.core.cql PreparedStatement BoundStatement Row ResultSet ColumnDefinitions]
   [java.util.concurrent ConcurrentHashMap]))

;; ============================================================================
;; PreparedStatement Cache
;; ============================================================================

(def ^ConcurrentHashMap prepared-cache
  "Cache of PreparedStatement instances. Key: session-hash::cql"
  (ConcurrentHashMap.))

(defn- cache-key [^CqlSession session cql]
  (str (System/identityHashCode session) "::" cql))

(defn- get-or-prepare
  "Gets a PreparedStatement from cache or prepares it"
  [^CqlSession session cql]
  (.computeIfAbsent prepared-cache
                    (cache-key session cql)
                    (reify java.util.function.Function
                      (apply [_ _]
                        (.prepare session cql)))))

(defn clear-prepared-cache!
  "Clears the PreparedStatement cache (useful for testing)"
  []
  (.clear prepared-cache))

;; ============================================================================
;; Row Conversion
;; ============================================================================

(defn- row->map
  "Converts a Cassandra Row to a Clojure map (optimized with transients)"
  [^Row row]
  (let [^ColumnDefinitions column-defs (.getColumnDefinitions row)
        size (.size column-defs)]
    (loop [i 0
           m (transient {})]
      (if (< i size)
        (let [col-def (.get column-defs i)
              col-name (keyword (.asInternal (.getName col-def)))
              col-value (.getObject row i)]
          (recur (inc i) (assoc! m col-name col-value)))
        (persistent! m)))))

(defn- result->maps
  "Converts a ResultSet to a lazy sequence of maps"
  [^ResultSet result]
  (map row->map (iterator-seq (.iterator result))))

(defn execute
  "Executes a CQL query using PreparedStatement (cached) and returns results as Clojure data structures.
   
   Parameters:
   - client: CqlSession instance
   - q-str: CQL query string (can use named parameters like :id, :name)
   - params: (optional) map of parameters {:id \"123\" :name \"Alice\"}
   
   Examples:
   (execute session \"SELECT * FROM users\")
   ;; => ({:id \"1\" :name \"Alice\"} {:id \"2\" :name \"Bob\"})
   
   (execute session \"SELECT * FROM users WHERE id = :id\" {:id \"1\"})
   ;; => ({:id \"1\" :name \"Alice\"})
   
   (execute session \"INSERT INTO users (id, name) VALUES (:id, :name)\" {:id \"1\" :name \"Alice\"})
   ;; => ()
   
   PreparedStatements are automatically cached per session for optimal performance."
  ([^CqlSession client q-str params]
   (let [{:keys [cql params]} (if (nil? params)
                                {:cql q-str :params []}
                                (q/prepare q-str params))
         ^PreparedStatement ps (get-or-prepare client cql)
         ^BoundStatement bs (.bind ps (to-array params))
         result (.execute client bs)]
     (result->maps result)))

  ([^CqlSession client q-str]
   (execute client q-str nil)))
