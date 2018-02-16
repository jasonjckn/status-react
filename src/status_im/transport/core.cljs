(ns status-im.transport.core
  (:require [status-im.transport.message :as message]
            [re-frame.core :as re-frame]))

(defn stop-whisper! []
  (stop-watching-all!)
  (reset-all-pending-messages!)
  (reset-keys!))

(defn init-whisper!
  [{:keys [identity web3
           contacts profile-keypair pending-messages]
    :as   options}]
  {:pre [(valid? ::options options)]}
  (debug :init-whisper)
  (stop-whisper!)

  (f/add-filter! web3
                 {:privateKeyID identity
                  :topics [message/ping-topic]}
                 (fn [js-error js-message]
                   (re-frame/dispatch [:protocol/receive-whisper-message js-error js-message]))))
