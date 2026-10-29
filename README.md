# playbook

This is a thin set of utility functions I've found to be useful in
several different Clojure programs. In the spirit of a sport team's
playbook or support group's runbook, this aims to provide useful
answers to questions and make it easier to move on to whatever's more
important.

It's the base for applications whether or not they face the web;
web-facing applications build on ectcommon, which builds on this. General functionality covers:

* Use [`cprop`](https://github.com/tolitius/cprop) to load
  configuration files in a standard and configurable way.
* Easily set up a [`timber`](https://github.com/ptaoussanis/timbre)
  logger with some nice defaults.
* Run scheduled jobs (`playbook.scheduler`).
* Render Markdown templates with `{{variables}}` (`playbook.template`).
* Send e-mail from a background queue (`playbook.mail`).
* Provide a set of basic utility functions for common parsing and
  error handling tasks.

The first two bullet points are largely driven by a general approach
I've taken [packaging small Clojure apps](https://www.mschaef.com/packaging_small_clojure_apps).

## License

Copyright © 2015-2025 [Michael Schaeffer](http://www.mschaef.com/)

All Rights Reserved.

