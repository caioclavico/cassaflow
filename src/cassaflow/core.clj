(ns cassaflow.core
  (:require
   [cassaflow.query :as q])
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [com.datastax.oss.driver.api.core.cql SimpleStatement Row ResultSet]))

(defn- row->map
  "Converts a Cassandra Row to a Clojure map"
  [^Row row]
  (let [column-defs (.getColumnDefinitions row)]
    (reduce
     (fn [m i]
       (let [col-def (.get column-defs i)
             col-name (keyword (str (.getName col-def)))
             col-value (.getObject row i)]
         (assoc m col-name col-value)))
     {}
     (range (.size column-defs)))))

(defn- result->maps
  "Converts a ResultSet to a lazy sequence of maps"
  [^ResultSet result]
  (map row->map (iterator-seq (.iterator result))))

(defn execute
  "Executes a CQL query and returns results as Clojure data structures.
   
   Parameters:
   - client: CqlSession instance
   - q-str: CQL query string (can use named parameters like :id, :name)
   - params: (optional) map of parameters {:id \"123\" :name \"Alice\"}
   - opts: (optional) map of options:
     - :raw? - if true, returns raw ResultSet (default: false)
     - :one? - if true, returns single map instead of sequence (default: false)
   
   Examples:
   (execute session \"SELECT * FROM users\")
   ;; => ({:id \"1\" :name \"Alice\"} {:id \"2\" :name \"Bob\"})
   
   (execute session \"SELECT * FROM users WHERE id = :id\" {:id \"1\"})
   ;; => ({:id \"1\" :name \"Alice\"})
   
   (execute session \"SELECT * FROM users WHERE id = :id\" {:id \"1\"} {:one? true})
   ;; => {:id \"1\" :name \"Alice\"}"
  ([^CqlSession client q-str params opts]
   (let [{:keys [cql params]} (q/prepare q-str params)
         stmt (-> (SimpleStatement/builder cql)
                  (.addPositionalValues (into-array Object params))
                  (.build))
         result (.execute client stmt)]
     (cond
       (:raw? opts) result
       (:one? opts) (first (result->maps result))
       :else (result->maps result))))

  ([^CqlSession client q-str params]
   (execute client q-str params {}))

  ([^CqlSession client q-str]
   (let [result (.execute client q-str)]
     (result->maps result))))
