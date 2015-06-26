(ns tom.yam
  (require [plumbing.core :refer [fnk]]
           [plumbing.graph :as graph]
           [plumbing.fnk.pfnk :as pfnk]
           [clojure.walk :as walk]
           [schema.core :as s]))

;;
;; the soup
;;

(def ^:dynamic *components* nil)

(defn start
  ([dependencies]
   (start dependencies {}))
  ([dependencies config]
   (binding [*components* (atom [])]
     (let [g ((graph/compile dependencies) {:config config})]
       (with-meta g {::shutdown (reverse @*components*)})))))

(defn stop [soup]
  (doseq [c (::shutdown (meta soup))]
    (println "->stop" c)))

(defn- strip [x]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (dissoc x s/Keyword)
        x))
    x))

(defn dependencies [x]
  (some-> x pfnk/input-schema strip))

(defmacro module [name [s a & rest]]
  (let [injection `((do
                      (println "->start" ~name)
                      (swap! *components* conj ~name)))]
    (concat [s a] injection rest)))

;;
;; Spike
;;

(def database* (module :db (fnk [[:config [:db url :- String]]]
                             {:conn (str "connected to " url)})))

(def http*     (module :http (fnk [[:config [:http port :- Long]] routes]
                               {:server (str "http-server running at " port ", serving " routes)})))

(def routes*   (module :routes (fnk [[:db conn]]
                                 (str "a route using the db conn: " conn))))

(def config {:db {:url "jdbc:something"}
             :http {:port 3000}})

(def graph (start {:db database*
                  :http http*
                  :routes routes*} config))

(println)
(println "web server:" (:db graph))
(println "database dependencies:" (dependencies database*))
(println)

(stop graph)
