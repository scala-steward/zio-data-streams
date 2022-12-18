# zdata
ZIO Streams integration with JDK DataInput and DataOutput format

Open Questions:

  * Should EOFException be exposed to users, or not? NegativeLengthException?

TODO:

  * A few methods from DataInput / DataOutput are not implmented (some on purpose and documented, some just TODO)
  * Could this be made to work with ScalaNative and ScalaJS, too?
  * Higher level tests showed memory / GC pressure higher compared to code using DataInputStream / DataOutputStream...
