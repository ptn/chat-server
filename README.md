# chat

A chat server. This is a WIP, it currently only works as a simple echo
server, most of the work so far has been in the implementation of the
select loop.

## Usage

1. `lein repl`
2. Run `(chat.select/run (chat-server 4555) select-handler)`
3. In another terminal: `telnet localhost 4555`
4. Type stuff, you should see it echoed back to you.

## License

Copyright © 2014 Pablo Torres

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
