This is a copy of the chronicle-flux library that hasn't been maintained since 2018. There are two reasons to import it
to the our repo:

1. It is not compatible with the later versions of Chronicle
2. The access to queue tailer is done in different thread than the consumption of it. This kind of unsafe behavior has
   been blocked on the newer versions of Chronicle due to the tailers not being thread-safe.

Implementing these changes required very slight modifications to the original code.
