(ns status-im.transport.handlers
  (:require [re-frame.core :as re-frame] 
            [status-im.utils.handlers :as handlers]
            [status-im.transport.message.core :as message]
            [status-im.transport.core :as transport]
            [status-im.chat.models :as models.chat]
            [status-im.utils.datetime :as datetime]
            [taoensso.timbre :as log]
            [status-im.transport.utils :as web3.utils]
            [cljs.reader :as reader]
            [status-im.transport.message.transit :as transit]))

(re-frame/reg-fx
  :stop-whisper
  (fn []
    (transport/stop-whisper!)))

(re-frame/reg-fx
  ::init-whisper
  (fn [{:keys [web3 public-key groups status contacts pending-messages]}]
    (transport/init-whisper!
     {:web3                        web3
      :identity                    public-key
      :pending-messages            pending-messages})))



(handlers/register-handler-fx
  :protocol/receive-whisper-message
  [re-frame/trim-v]
  (fn [cofx [js-error js-message chat-id]]
    (let [{:keys [payload sig]} (js->clj js-message :keywordize-keys true)
          status-message        (-> payload
                                    web3.utils/to-utf8
                                    transit/deserialize)]
      (when (and sig status-message)
        (message/receive status-message cofx (or chat-id sig) sig)))))

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
