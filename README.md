# [Work In Progress] Rusty Object Notation

Clojure implementation of [ron](https://github.com/ron-rs/ron)

Still in-design:

- [ ] Finish API Design
- [ ] Write tests
- [ ] Port code/tests to `cljs`
- [ ] Check `sci`/`native-image` support
- [ ] Do a `JS` performatic review
- [ ] Do a `JVM` performatic review
- [ ] Check DOC: Should be possible import just reader or just writer
- [ ] Decide `artifact` and `namespace` names
- [ ] First release 
- [ ] Home page/playground `edn<>ron`

Current design questions:

- How to represent `Foo(42, b: 1)`
```clojure 
(ron/read-string
  "Foo(42, b: 1)"
  {:index-names '{Foo [:a]}})
;; => {:a 42, :Foo/b 1}
```

- How to represent `Foo(42)`
```clojure 
(ron/read-string
  "Foo(42)")
;; => {:Foo [42]}
```
```clojure 
(ron/read-string
  "Foo(42)")
;; => #Foo(42)
```
```clojure 
(ron/read-string
  "Foo(42)")
;; => #Foo(42)
```