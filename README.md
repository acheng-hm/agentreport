# agent-log-report

## Prerequisite

Matcha's agent-fn logs the member state on read and write like this:

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
                (l/debugf "zxcv agent read-from-redis? %s returned %s" read-from-redis? the-state)
                the-state))
          ;; change agent state to new state
          ([new-state] (let [result (->> (t/time-now)
                                         (assoc new-state :updated-time)
                                         (reset! state))
                             member-id (:member-id @state)
                             wrote-to-redis? (when-not (nil? member-id)
                                               (set-redis-member-by-id member-id @state)
                                               true)]
                         (l/debugf "zxcv agent wrote-to-redis? %s wrote %s" wrote-to-redis? result)
                         result)))))

## Usage

FIXME: explanation

    $ lein run [path to matcha's app.log] [path to APPEND report]

example:

    rm report
    lein run app.log report
