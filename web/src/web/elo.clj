(ns web.elo)

(def *k-factor* 32)

(def *starting-score* 1200)

(defn expected-result [a b]
  (/ 1 (+ 1 (Math/pow 10 (/ (- a b) 400)))))

(defn score-change [a b result]
  (* *k-factor* (- result (expected-result a b))))