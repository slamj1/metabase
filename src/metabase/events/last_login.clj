(ns metabase.events.last-login
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [metabase.config :as config]
            [metabase.db :as db]
            [metabase.events :as events]
            [metabase.models.user :refer [User]]
            [metabase.util :as u]))


(def last-login-topics
  "The `Set` of event topics which are subscribed to for use in last login tracking."
  #{:user-login})

(def ^:private last-login-channel
  "Channel for receiving event notifications we want to subscribe to for last login events."
  (async/chan))


;;; ## ---------------------------------------- EVENT PROCESSING ----------------------------------------


(defn process-last-login-event
  "Handle processing for a single event notification received on the last-login-channel"
  [last-login-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic :topic object :item} last-login-event]
      (log/info object)
      ;; just make a simple attempt to set the `:last_login` for the given user to now
      (when-let [user-id (:user_id object)]
        (db/upd User user-id :last_login (u/new-sql-timestamp))))
    (catch Throwable e
      (log/warn (format "Failed to process sync-database event. %s" (:topic last-login-event)) e))))



;;; ## ---------------------------------------- LIFECYLE ----------------------------------------


;; this is what actually kicks off our listener for events
(when (config/is-dev?)
  (log/info "Starting last-login events listener")
  (events/start-event-listener last-login-topics last-login-channel process-last-login-event))
