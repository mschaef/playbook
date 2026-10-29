;; Copyright (c) 2015-2026 Michael Schaeffer
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns playbook.mail
  "A background mailer. Messages are queued and sent, one at a time,
  through the SMTP server in the :smtp configuration."
  (:use playbook.core)
  (:require [taoensso.timbre :as log]
            [postal.core :as postal]
            [playbook.config :as config]))

(defn start-mailer
  "Start the mailer's sending thread and return the mailer, to pass to
  queue-message. When :smtp :enabled is false, messages are logged and
  kept instead of sent (so they can still be looked up by
  queued-message)."
  []
  (let [mailer {:queue (java.util.concurrent.LinkedBlockingQueue.)
                :store (atom {})}]

    (with-daemon-thread 'mailer
      (let [smtp (config/cval :smtp)]
        (while true
          (let [uuid (.take (:queue mailer))
                message (@(:store mailer) uuid)]
            (if (:enabled smtp)
              (with-exception-barrier 'send-mail
                (log/info "Mailing message: " message)
                (swap! (:store mailer) dissoc uuid)
                (postal/send-message {:host (:host smtp)
                                      :user (:user smtp)
                                      :pass (:password smtp)
                                      :ssl true}
                                     message))
              (log/warn "E-mail disabled. Message not mailed: " message))))))
    mailer))

(defn queue-message
  "Queue a postal message (:to, :subject, :body, ...) for sending, and
  return its UUID. :from defaults to the :smtp :from configuration
  value."
  [mailer message]
  (let [uuid (.toString (java.util.UUID/randomUUID))
        message (merge {:from (config/cval :smtp :from)}
                       message
                       {:uuid uuid})]
    (log/info "Queuing message: " message)
    (swap! (:store mailer) assoc uuid message)
    (.add (:queue mailer) uuid)
    uuid))

(defn queued-message
  "The message queued under uuid, if it hasn't been sent yet."
  [mailer uuid]
  (@(:store mailer) uuid))
