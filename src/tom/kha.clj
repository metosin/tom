(ns tom.kha
  (require [plumbing.core :refer [fnk]]
           [plumbing.graph :as graph]
           [plumbing.fnk.pfnk :as pfnk]
           [clojure.walk :as walk]
           [schema.core :as s]))

;;
;; the soup
;;

(defn soup
  ([dependencies]
   (soup dependencies {}))
  ([dependencies config]
   ((graph/compile (apply merge dependencies)) {:config config})))

(defn strip [x]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (dissoc x s/Keyword)
        x))
    x))

(defn dependencies [x]
  (some-> x vals first pfnk/input-schema strip))

;;
;; Spike
;;

(def database* {:db (fnk [[:config [:db url :- String]]]
                      (println "starting the database!")
                      {:conn (str "connected to " url)})})

(def http*     {:http (fnk [[:config [:http port :- Long]] routes]
                        (println "starting the server!")
                        {:server (str "http-server running at " port ", serving " routes)})})

(def routes*   {:routes (fnk [[:db conn]]
                          (println "compiling the routes!")
                          (str "a route using the db conn: " conn))})


(def config {:db {:url "jdbc:something"}
             :http {:port 3000}})

(def graph (soup [database* http* routes*] config))

(println "-->")
(println "web server:" (:db graph))
(println "database dependencies:" (dependencies database*))
