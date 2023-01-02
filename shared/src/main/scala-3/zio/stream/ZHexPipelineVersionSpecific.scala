package zio.stream

import java.io.EOFException

private[stream] object ZHexPipelineVersionSpecific {
  type HexDecodeException = EOFException | InvalidHexChar
}
