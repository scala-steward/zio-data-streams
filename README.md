# ZIO Data Streams

ZIO Streams integration with JDK DataInput and DataOutput format

![CI][Badge-CI]

Open Questions:

  * Should EOFException be exposed to users, or not? NegativeLengthException?

TODO:

  * A few methods from DataInput / DataOutput are not implmented (some on purpose and documented, some just TODO)
  * Could this be made to work with ScalaNative and ScalaJS, too?
  * Higher level tests showed memory / GC pressure higher compared to code using DataInputStream / DataOutputStream...

---

### Legal

Copyright 2022 Gregor Purdy. All rights reserved.

[Badge-CI]: https://github.com/gnp/zdata/workflows/CI/badge.svg
