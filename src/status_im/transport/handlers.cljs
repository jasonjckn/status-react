(ns status-im.transport.handlers
  (:require [re-frame.core :as re-frame]
            [status-im.utils.handlers :as handlers]
            [status-im.transport.message-cache :as message-cache]
            [status-im.transport.message.core :as message]
            [status-im.transport.core :as transport]
            [status-im.chat.models :as models.chat]
            [status-im.utils.datetime :as datetime]
            [taoensso.timbre :as log]
            [status-im.transport.utils :as web3.utils]
            [cljs.reader :as reader]))

(re-frame/reg-fx
  :stop-whisper
  (fn [] (protocol/stop-whisper!)))

(re-frame/reg-fx
  ::init-whisper
  (fn [{:keys [web3 public-key groups status contacts pending-messages]}]
    (transport/init-whisper!
     {:web3                        web3
      :identity                    public-key
      :pending-messages            pending-messages})))

;; TODO (yenda) add handlers for cutsom values https://github.com/cognitect/transit-cljs/wiki/Getting-Started
(def reader (transit/reader :json))
(def writer (transit/writer :json))

(defn serialize [o] (transit/write writer o))
(defn deserialize [o] (try (transit/read reader o) (catch :default e nil)))

(defn get-message-id [status-message]
  (web3.utils/sha3 status-message))

(defn parse-payload [message]
  (let [{:keys [payload sig]} (js->clj js-message :keywordize-keys true)
        status-message        (-> payload
                                  web3.utils/to-utf8)]
    {:signature         sig
     :message-id        (get-message-id status-message)
     :status-message    (deserialize status-message)}))

(defn deduplication [{:keys [message-id status-message] :as message}]
  (when-not (message-cache/exists? message-id)
    (message-cache/add! message-id)
    message))

(handlers/register-handler-fx
  :protocol/receive-whisper-message
  [re-frame/trim-v]
  (fn [{:keys [db]} [js-error js-message]]
    (let [{:keys [signature status-message message-id]} (some-> js-message
                                                                parse-payload
                                                                deduplication)]
      (when (and signature status-message message-id)
        (receive status-message signature message-id)))))

(handlers/register-handler-fx
  :protocol/send-status-message
  [re-frame/trim-v]
  (fn [{:keys [db]} [chat-id status-message]]
    {:shh/post {:web3          (:web3 db)
                ;;TODO (yenda) not the right place to serialize
                :message (serialize (send status-message))}}))

(handlers/register-handler-fx
  :protocol/send-status-message-success
  [re-frame/trim-v]
  (fn [{:keys [db] :as cofx} [_ resp]]
    (log/debug :send-status-message-success resp)))

(handlers/register-handler-fx
  :protocol/send-status-message-error
  [re-frame/trim-v]
  (fn [{:keys [db] :as cofx} [err]]
    (log/error :send-status-message-error err)))
