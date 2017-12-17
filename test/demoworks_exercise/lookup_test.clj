(ns demoworks-exercise.lookup-test
  (:require [demoworks-exercise.lookup :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(def good-entry (sut/->OCDEntry "123 Place St" "oakland" "ca" "94606" "alameda" nil))

(deftest data-rendering-tests
  (let [expected-country "ocd-division/country:us"
        expected-state   (str expected-country "/state:ca")
        expected-city    (str expected-state "/place:oakland")
        expected-county  (str expected-state "/county:alameda")]
    (testing "Can we correctly format `OCDEntry`s with a single format?"
      (is (= expected-country (sut/render-ocd good-entry [])) "Should get back our country string")
      (is (= expected-state   (sut/render-ocd good-entry sut/state-keys)) "Should get back our state string")
      (is (= expected-city    (sut/render-ocd good-entry sut/city-keys)) "Should get back our city string")
      (is (= expected-county  (sut/render-ocd good-entry sut/county-keys))) "Should get back our county string")

    (testing "Can we parse one OCDEntry into a correct, single string?"
      (let [expected-single-string (str expected-state "," expected-city)]
        (is (= expected-single-string (sut/render-ocds good-entry)) "Is our rendered full string as we'd expect?")))))

;; TODO: I probably wont have time, but mocking out HTTP requests to be sure
;; they're rock-solid would be a very good thing to do.
(deftest request-tests
  (let [good-edn "{:hi 5}"
        bad-edn  "{:hi"]
    (testing "Can we parse EDN, returning it if so, quietly logging and nil-ing if not?"
      (is (= {:hi 5} (sut/read-body good-edn)))
      (is (= nil (sut/read-body bad-edn)))))

  (testing "Can we clean up incoming data correctly?"
    (is (= "los_angeles" (sut/clean-city-name "Los Angeles"))))

  (testing "Can we parse form data into something we can use?"
    (let [form-params {"__anti-forgery-token" "M09AMWMDlk/Rx5A/gUD7s9DH3PrMX7Jt25DNTZ+vZV1dzJD4zqWqqIWf7GyH7QBVKgAOfVHOMG1dxNvI", "street" "1234 Place St", "street-2" "", "city" "City", "state" "CA", "zip" "12345"}
          expected-entry (sut/->OCDEntry "1234 Place St" "city" "ca" "12345" nil nil)]
      (is (= expected-entry (sut/parse-form-params-to-entry form-params)))))

  )
