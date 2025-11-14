(ns cassaflow.core
  (:require
   [cassaflow.query :as q])
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [com.datastax.oss.driver.api.core.cql SimpleStatement]))

(defn execute
  ([^CqlSession client q-str params]
   (let [{:keys [cql params]} (q/prepare q-str params)
         stmt (-> (SimpleStatement/builder cql)
                  (.addPositionalValues (into-array Object params))
                  (.build))]
     (.execute client stmt)))

  ([^CqlSession client q-str]
   (.execute client q-str)))
