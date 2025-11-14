(ns cassaflow.query
  (:require [clojure.string :as str]))

(def param-regex #":[a-zA-Z0-9_-]+")

(defn extract-params [q]
  (->> (re-seq param-regex q)
       (map #(subs % 1))
       (map keyword)))

(defn replace-placeholders [q]
  (str/replace q param-regex "?"))

(defn prepare [q params]
  (let [ordered (extract-params q)]
    {:cql    (replace-placeholders q)
     :params (map params ordered)}))
