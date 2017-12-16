(ns demoworks-exercise.us-state-test
  (:require [clojure.test :refer :all]
            [demoworks-exercise.us-state :refer :all]))

(deftest postal-abbreviations-test
  (testing "there are 61 states, territories, military abbreviations, etc."
    (is (= 61
           (count postal-abbreviations))))
  (testing "some of the best states and districts are present"
    ;; TODO: Add my own favorite state to this ;D (Skipping for now to maintain anonymity.)
    (is (every? (set postal-abbreviations)
                #{"CO" "NY" "CA" "KS" "DC" "IL" "WA"}))))
