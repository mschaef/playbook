# playbook

This is a thin set of utility functions I've found to be useful in
several different Clojure programs. In the spirit of a sport team's
playbook or support group's runbook, this aims to provide useful
answers to questions and make it easier to move on to whatever's more
important.

General functionality is limited to three areas at the moment:

* Use [`cprop`](https://github.com/tolitius/cprop) to load
  configuration files in a standard and configurable way.
* Easily set up a [`timber`](https://github.com/ptaoussanis/timbre)
  logger with some nice defaults.
* Provide a set of basic utility functions for common parsing and
  error handling tasks.

The first two bullet points are largely driven by a general approach
I've taken [packaging small Clojure apps](https://www.mschaef.com/packaging_small_clojure_apps).

## License

Copyright © 2015-2025 [Michael Schaeffer](http://www.mschaef.com/)

All Rights Reserved.

