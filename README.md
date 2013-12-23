Chromecast Demo
===================

Simple demo using Google Cast API.

## Install

* Copy receiver/* into leapcast folder.
* Add the following code to leapcast/apps/default.py

```python
    class ChromecastDemo(LEAPfactory):
      url = "file:///<path/to/leapcast>/receiver/demo.html"
```
