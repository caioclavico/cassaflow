(ns cassaflow.client
  (:import
   [com.datastax.oss.driver.api.core CqlSession]
   [java.net InetSocketAddress]))

(defn connect
  [{:keys [host port keyspace]
    :or   {host "127.0.0.1"
           port 9042}}]

  (let [builder (-> (CqlSession/builder)
                    (.addContactPoint (InetSocketAddress. host port))
                    (.withLocalDatacenter "datacenter1"))]
    (if keyspace
      (.withKeyspace builder keyspace)
      builder)

    (.build builder)))
