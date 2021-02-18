# agent-log-report

## Prerequisite

Matcha's agent-fn logs the member state on read and write like this:

     (defn get-trace[]
       (->> (Exception.)
            .getStackTrace
            (filter #(clojure.string/includes? (.getClassName %) "payoff"))))

     (defn- agent-fn
       [s]
       (let [state (atom s)]
         (fn
           ;; get agent state
           ([] (let [member-id (:member-id @state)
                     read-from-redis? (when-not (nil? member-id)
                                        (reset! state (get-redis-member-by-id member-id))
                                        true)
                     the-state @state]
                 (l/debug (pr-str {:state the-state :zxcv "r" :redis? read-from-redis? :trace (get-trace)}))
                 the-state))
           ;; change agent state to new state
           ([new-state] (let [result (->> (t/time-now)
                                          (assoc new-state :updated-time)
                                          (reset! state))
                              member-id (:member-id @state)
                              wrote-to-redis? (when-not (nil? member-id)
                                                (set-redis-member-by-id member-id @state)
                                                true)]
                          (l/debug (pr-str {:state result :zxcv "w" :redis? wrote-to-redis? :trace (get-trace)}))
                          result)))))

## Usage


    $ lein run [path to matcha's app.log] [path to APPEND report]

example:

    rm report
    lein run app.log report
