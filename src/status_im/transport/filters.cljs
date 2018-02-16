(ns status-im.transport.filters
  (:require [status-im.transport.utils :as utils]
            [status-im.utils.config :as config]
            [taoensso.timbre :as log]))


(defonce filters (atom {}))

;; NOTE(oskarth): Due to perf we don't want a single topic for all messages,
;; instead we want many. We need a way for user A and B to agree on which topics
;; to use. By using first 10 characters of the pub-key, we construct a topic
;; that requires no coordination for 1-1 chats.
(defn identity->topic [identity]
  (apply str (take 10 identity)))

(defn get-topics [& [identity]]
  [status-topic])

(defn remove-filter! [web3 options]
  (when-let [filter (get-in @filters [web3 options])]
    (.stopWatching filter
                   (fn [error _]
                     (when error
                       (log/warn :remove-filter-error options error))))
    (log/debug :stop-watching options)
    (swap! filters update web3 dissoc options)))

(defn add-shh-filter!
  [web3 options callback]
  (let [filter   (.newMessageFilter (utils/shh web3) (clj->js options)
                                    callback
                                    #(log/warn :add-filter-error (.stringify js/JSON (clj->js options')) %))]
    (swap! filters assoc-in [web3 options] filter)))

(defn add-filter!
  [web3 {:keys [topics to] :as options} callback]
  (remove-filter! web3 options)
  (log/debug :add-filter options)
  (add-shh-filter! web3 options callback))

(defn remove-all-filters! []
  (doseq [[web3 filters] @filters]
    (doseq [options (keys filters)]
      (remove-filter! web3 options))))
