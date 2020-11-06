migrate:
	lein run migrate

kondo:
	clj-kondo --lint src test --cache false

fmt:
	lein cljfmt check

ancient:
	lein ancient

ci: kondo fmt ancient
