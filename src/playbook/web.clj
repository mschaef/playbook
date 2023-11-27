(ns playbook.web
  (:use compojure.core))

(defmacro when-let-route [ bindings & route-table ]
  `(if-let ~bindings
     (routes ~@route-table)
     (routes)))

