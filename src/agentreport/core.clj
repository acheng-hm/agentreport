(ns agentreport.core
  (:require [cheshire.core :as json]
            [clojure.data :as d]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(defn log
  "pretty prints a value to path"
  [path val]
  (spit path (with-out-str (pp/pprint val)) :append true))

(defn parse-event [event-json]
  (let [event (json/parse-string event-json)
        [_ op state-edn] (re-matches #"zxcv agent ([rw])[^{]*(.*)" (get event "msg"))
        state (as-> state-edn $
                (str/replace $ #"#object[^\"]*([^\]]*)\]" "$1")
                (edn/read-string $))
        meta (str (get event "host") "---" op "---" (get event "timestamp") "----------------")]
    (-> event
        (dissoc "level" "timestamp" "host" "msg")
        (assoc :meta meta
               :state state))))

(defn only-updated-time-changed? [loss gain]
  (and (= #{:updated-time} (-> loss keys set))
       (= #{:updated-time} (-> gain keys set))))

(defn report-state-change [out acc parsed-event]
  (let [prev-state (if (contains? acc :state) (:state acc) {})
        [loss gain _] (clojure.data/diff prev-state (:state parsed-event))]
    (cond
      (not(contains? acc :state)) (log out (assoc parsed-event :note "INITIAL STATE"))
      (and (nil? loss) (nil? gain)) (log out (-> parsed-event
                                                 (dissoc :state)
                                                 (update :meta #(str % "NO CHANGE IN STATE"))))
      (only-updated-time-changed? loss gain) (log out (-> parsed-event
                                                          (dissoc :state)
                                                          (update :meta  #(str % "new updated time:" (:updated-time gain)))))
      :else (log out (assoc parsed-event :---loss--- loss :---gain--- gain)))
    parsed-event))

(defn make-complete-report
  "reads from path. _appends_ to out."
  [path out]
  (with-open [rdr (clojure.java.io/reader path)]
    (->> (line-seq rdr)
         (filter #(clojure.string/includes? % "zxcv"))
         (map parse-event)
         (reduce (partial report-state-change out) {}))))

(defn -main
  [& args]
  (if (= 2 (count args))
    (make-complete-report (nth args 0) (nth args 1))
    (printf "Need 2 args: [path to matcha's app.log] [path to APPEND report]\nGot: %s\n\n" args)))
