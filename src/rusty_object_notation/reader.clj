(ns rusty-object-notation.reader
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]))

(defn do-next
  [{::keys [next-chars]
    :as    env}]
  (assoc env
    ::dispatch-char (first next-chars)
    ::next-chars (rest next-chars)))

(declare read-impl)

(defn string-reader-impl
  [{::keys [next-chars]
    :as    env}]
  (let [[xs [_ dispatch-char & next-chars]] (split-with (complement #{\"})
                                                        next-chars)]
    (assoc env
      ::next-chars next-chars
      ::dispatch-char dispatch-char
      ::value (apply str xs))))

(defn numeric?
  [x]
  (contains? (set "0123456789-.") x))

(defn number-reader-impl
  [{::keys [dispatch-char next-chars]
    :as    env}]
  (let [[ns next-chars] (split-with numeric?
                                    next-chars)]
    (assoc env
      ::next-chars (rest next-chars)
      ::dispatch-char (first next-chars)
      ::value (edn/read-string (apply str dispatch-char ns)))))


(defn true-reader-impl
  [{::keys [next-chars]
    :as    env}]
  (let [[#_t _r _u _e dispatch-char & next-chars] next-chars]
    (assoc env
      ::next-chars next-chars
      ::dispatch-char dispatch-char
      ::value true)))

(defn false-reader-impl
  [{::keys [next-chars]
    :as    env}]
  (let [[#_f _a _l _s _e dispatch-char & next-chars] next-chars]
    (assoc env
      ::next-chars next-chars
      ::dispatch-char dispatch-char
      ::value false)))

(defn null-reader-impl
  [{::keys [next-chars]
    :as    env}]
  (let [[#_n _u _l _l dispatch-char & next-chars] next-chars]
    (assoc env
      ::next-chars next-chars
      ::dispatch-char dispatch-char
      ::value nil)))

(defn whitespace?
  [x]
  (contains? #{\tab
               \newline
               \return
               \space}
             x))

(defn read-symbol-impl
  [{::keys [dispatch-char next-chars] :as env}]
  (let [[sym-vs next-chars] (split-with (fn [x]
                                          (not (or (whitespace? x)
                                                   (contains? #{\( \{ \[ \:} x))))
                                        (cons dispatch-char next-chars))]
    (assoc env
      ::next-chars (rest next-chars)
      ::dispatch-char (first next-chars)
      ::value (symbol (apply str sym-vs)))))

(defn comma-or-colon
  [{::keys [dispatch-char]
    :as    env}]
  (cond
    (contains? #{\: \,} dispatch-char) env
    (whitespace? dispatch-char) (recur (do-next env))
    :else env))


(defn collection-reader-impl
  [{::keys [with-namespace
            dispatch-char]
    :as    env}]
  (let [end-of-coll ({\( \)
                      \{ \}
                      \[ \]}
                     dispatch-char)]
    (loop [{::keys [coll dispatch-char kv?]
            :as    env} (assoc (do-next env)
                          ::kv? (= end-of-coll \})
                          ::coll (transient []))]
      (cond
        (nil? dispatch-char) (throw (ex-info "eof" env))
        (whitespace? dispatch-char) (recur (do-next env))
        (contains? #{end-of-coll} dispatch-char) (assoc (do-next env)
                                                   ::value (if kv?
                                                             (into {} (persistent! coll))
                                                             (if with-namespace
                                                               (tagged-literal
                                                                 with-namespace
                                                                 (persistent! coll))
                                                               (persistent! coll))))
        :else (let [{key ::value
                     :as env} (read-impl env)
                    {comma-or-colon ::dispatch-char
                     :as            env} (comma-or-colon env)
                    kv? (= \: comma-or-colon)
                    {value ::value
                     :as   env} (if kv?
                                  (read-impl (do-next env))
                                  env)]
                (recur (assoc env
                         ::kv? kv?
                         ::coll (conj! coll
                                       (if kv?
                                         [(if (and with-namespace
                                                   (ident? key))
                                            (keyword (str with-namespace)
                                                     (str key))
                                            key)
                                          value]
                                         key)))))))))


(defn read-impl
  [{::keys [dispatch-char with-namespace]
    :as    env}]
  (cond
    (contains? #{\:} dispatch-char) (assoc env ::value with-namespace)
    (whitespace? dispatch-char) (recur (do-next env))
    (contains? #{\"} dispatch-char) (string-reader-impl env)
    (contains? #{\t} dispatch-char) (true-reader-impl env)
    (contains? #{\f} dispatch-char) (false-reader-impl env)
    (numeric? dispatch-char) (number-reader-impl env)
    (contains? #{\n} dispatch-char) (null-reader-impl env)
    (contains? #{\(
                 \{
                 \[} dispatch-char) (collection-reader-impl env)
    :else (let [{::keys [value]
                 :as    env} (read-symbol-impl env)]
            (read-impl (assoc env
                         ::with-namespace value)))))

(defn read-string
  [s]
  (::value (read-impl {::dispatch-char (first s)
                       ::next-chars    (rest s)})))