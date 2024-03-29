/*
    Copyright (c) 2007-2014 Contributors as noted in the AUTHORS file

    This file is part of 0MQ.

    0MQ is free software; you can redistribute it and/or modify it under
    the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    0MQ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.zeromq;

import org.zeromq.ZActor.Actor;
import org.zeromq.ZAgent.SelectorCreator;
import org.zeromq.ZMQ.Socket;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import zmq.Msg;
import zmq.SocketBase;

/**
 * Implementation of a remotely controlled  proxy for 0MQ, using {@link ZActor}.
 * <br/>
 * The goals of this implementation are to delegate the creation of sockets
 * in a background thread via a callback interface to ensure their correct use
 * and to provide ultimately to end-users the following features.
 * <p>
 * Basic features:
 * <p><ul>
 * <li>Remote Control
 * <ul>
 * <li>Start:                                 <i>if was paused, flushes the pending messages</i>
 * <li>Pause:                                 <b><i>lets the socket queues accumulate messages according to their types</i></b>
 * <li>Stop:                                  <i>Shutdowns the proxy, can be restarted</i>
 * <li>Status:                                <i>Retrieves the status of the proxy</i>
 * <li>Cold Restart:                          <i>Closes and recreates the connections</i>
 * <li>{@link #restart(ZMsg) Hot Restart}:    <i>User-defined behavior with custom messages</i>
 * <li>{@link #configure(ZMsg) Configure}:    <i>User-defined behavior with custom messages</i>
 * <li>{@link #command(String, boolean) ...}: <i>Custom commands of your own</i>
 * <li>Exit:                                  <i>Definitive shutdown of the proxy and its control</i>
 * </ul>
 * All the non-custom commands can be performed in asynchronous or synchronous mode.
 * <br/>
 * <br/>
 * <li>Proxy mechanism ensured by pluggable pumps
 * <ul>
 * <li>with built-in low-level {@link ZmqPump} (zmq.ZMQ): useful for performances
 * <li>with built-in high-level  {@link ZPump}  (ZeroMQ): useful for {@link ZPump.Transformer message transformation}, lower performances
 * <li>with your own-custom proxy pump implementing a {@link Pump 1-method interface}
 * </ul>
 * </ul><p>
 * <p>
 * You can have all the above non-customizable features in about these lines of code:
 * <p>
 * <pre>
 * {@code
 * final ZProxy.Proxy provider = new ZProxy.SimpleProxy()
 * {
 * public Socket create(ZContext ctx, ZProxy.Plug place, Object[] args)
 * {
 * assert ("TEST".equals(args[0]);
 * Socket socket = null;
 * if (place == ZProxy.Plug.FRONT) {
 * socket = ctx.createSocket(ZMQ.ROUTER);
 * }
 * if (place == ZProxy.Plug.BACK) {
 * socket = ctx.createSocket(ZMQ.DEALER);
 * }
 * return socket;
 * }
 *
 * public void configure(Socket socket, ZProxy.Plug place, Object[] args)
 * {
 * assert ("TEST".equals(args[0]);
 * int port = -1;
 * if (place == ZProxy.Plug.FRONT) {
 * port = socket.bind("tcp://127.0.0.1:6660");
 * }
 * if (place == ZProxy.Plug.BACK) {
 * port = socket.bind("tcp://127.0.0.1:6661");
 * }
 * if (place == ZProxy.Plug.CAPTURE && socket != null) {
 * socket.bind("tcp://127.0.0.1:4263");
 * }
 * }
 * };
 *
 * ZProxy proxy = ZProxy.newProxy("ProxyOne", provider, "ABRACADABRA", Arrays.asList("TEST"));
 * }
 * <p></pre>
 * Once created, the proxy is not started. You have to perform first a start command on it.
 * This choice was made because it is easier for a user to start it with one line of code than for the code to internally handle
 * different possible starting states (after all, someone may want the proxy started but paused at first or configured in a specific way?)
 * and because the a/sync stuff was funnier. Life is unfair ...
 * Or maybe an idea is floating in the air?
 * <br/>
 * <p>
 * You can then use it like this:
 * <pre>
 * {@code
 * final boolean async = false, sync = true;
 * String status = null;
 * status = proxy.status();
 * status = proxy.pause(sync);
 * status = proxy.start(async);
 * ZMsg msg = proxy.restart(new ZMsg());
 * status = proxy.status(async);
 * status = proxy.stop(sync);
 * boolean here = proxy.sign();
 * ZMsg cfg = new ZMsg();
 * msg.add("CONFIG-1");
 * ZMsg rcvd = proxy.configure(cfg);
 * proxy.exit(async);
 * status = proxy.status(sync);
 * assert (!proxy.started());
 * }
 * <p></pre>
 * <p>
 * A {@link #command(Command, boolean) programmatic interface} with enums is also available.
 */
// Proxy for 0MQ.
public class ZProxy {
	/**
	 * Possible places for sockets in the proxy.
	 */
	public enum Plug {
		FRONT,   // The position of the frontend socket.
		BACK,    // The position of the backend socket.
		CAPTURE // The position of the capture socket.
	}

	// Contract for socket creation and customizable configuration in proxy threading.
	public interface Proxy {
		/**
		 * Creates and initializes (bind, options ...) the socket for the given plug in the proxy.
		 * The proxy will close them afterwards, and the context as well if not provided in the constructor.
		 * There is no need to keep a reference on the created socket or the context given in parameter.
		 *
		 * @param ctx   the context used for initialization.
		 * @param place the position for the future created socket in the proxy.
		 * @param args  the optional array of arguments that has been passed at the creation of the ZProxy.
		 * @return the created socket. Possibly null only for capture.
		 */
		Socket create(ZContext ctx, Plug place, Object[] args);

		/**
		 * Configures the given socket.
		 *
		 * @param socket the socket to configure
		 * @param place  the position for the socket in the proxy
		 * @param args   the optional array of arguments that has been passed at the creation of the ZProxy.
		 */
		void configure(Socket socket, Plug place, Object[] args);

		/**
		 * Performs a hot restart of the given socket.
		 * Usually an unbind/bind but you can use whatever method you like.
		 *
		 * @param cfg    the custom configuration message sent by the control.
		 * @param socket the socket to hot restart
		 * @param place  the position for the socket in the proxy
		 * @param args   the optional array of arguments that has been passed at the creation of the ZProxy.
		 * @return true to perform a cold restart instead, false to do nothing. All the results will be collected from calls for all plugs.
		 * If any of them returns true, the cold restart is performed.
		 */
		boolean restart(ZMsg cfg, Socket socket, Plug place, Object[] args);

		/**
		 * Configures the proxy with a custom message.
		 * <p>
		 * Note: you need to send one (1) mandatory custom response message with the pipe before the end of this call.
		 *
		 * @param pipe     the control pipe
		 * @param cfg      the custom configuration message sent by the control
		 * @param frontend the frontend socket
		 * @param backend  the backend socket
		 * @param capture  the optional capture socket
		 * @param args     the optional array of arguments that has been passed at the creation of the ZProxy.
		 * @return true to continue the proxy, false to exit
		 */
		boolean configure(Socket pipe, ZMsg cfg, Socket frontend, Socket backend,
						  Socket capture, Object[] args);

		/**
		 * Handles a custom command not recognized by the proxy.
		 * <p>
		 * Note: you need to send the current state at the end of the call.
		 *
		 * @param pipe     the control pipe
		 * @param cmd      the unrecognized command
		 * @param frontend the frontend socket
		 * @param backend  the backend socket
		 * @param capture  the optional capture socket
		 * @param args     the optional array of arguments that has been passed at the creation of the ZProxy.
		 * @return true to continue the proxy, false to exit
		 */
		boolean custom(Socket pipe, String cmd, Socket frontend, Socket backend, Socket capture, Object[] args);

		// this may be useful
		abstract class SimpleProxy implements Proxy {
			@Override
			public boolean restart(ZMsg cfg, Socket socket, Plug place,
								   Object[] args) {
				return true;
			}

			@Override
			public boolean configure(Socket pipe, ZMsg cfg,
									 Socket frontend, Socket backend, Socket capture, Object[] args) {
				return true;
			}

			@Override
			public boolean custom(Socket pipe, String cmd, Socket frontend, Socket backend, Socket capture, Object[] args) {
				return true;
			}
		}
	}

	/**
	 * Creates a new proxy in a ZeroMQ way.
	 * This proxy will be less efficient than the
	 * {@link #newZProxy(ZContext, String, org.zeromq.ZProxy.Proxy, String, Object...) low-level one}.
	 *
	 * @param ctx      the context used for the proxy.
	 *                 Possibly null, in this case a new context will be created and automatically destroyed afterwards.
	 * @param name     the name of the proxy. Possibly null.
	 * @param selector the creator of the selector used for the internal polling. Not null.
	 * @param sockets  the sockets creator of the proxy. Not null.
	 * @param args     an optional array of arguments that will be passed at the creation.
	 * @return the created proxy.
	 */
	// creates a new proxy
	public static ZProxy newZProxy(ZContext ctx, String name,
								   SelectorCreator selector, Proxy sockets, String motdelafin, Object... args) {
		return new ZProxy(ctx, name, selector, sockets, new ZPump(), null, args);
	}

	public static ZProxy newZProxy(ZContext ctx, String name, Proxy sockets, String motdelafin, Object... args) {
		return new ZProxy(ctx, name, null, sockets, new ZPump(), motdelafin, args);
	}

	/**
	 * Creates a new low-level proxy for better performances.
	 *
	 * @param ctx      the context used for the proxy.
	 *                 Possibly null, in this case a new context will be created and automatically destroyed afterwards.
	 * @param name     the name of the proxy. Possibly null.
	 * @param selector the creator of the selector used for the internal polling. Not null.
	 * @param sockets  the sockets creator of the proxy. Not null.
	 * @param args     an optional array of arguments that will be passed at the creation.
	 * @return the created proxy.
	 */
	// creates a new low-level proxy
	public static ZProxy newProxy(ZContext ctx, String name,
								  SelectorCreator selector, Proxy sockets, String motdelafin, Object... args) {
		return new ZProxy(ctx, name, selector, sockets, new ZmqPump(), motdelafin, args);
	}

	public static ZProxy newProxy(ZContext ctx, String name, Proxy sockets, String motdelafin, Object... args) {
		return new ZProxy(ctx, name, null, sockets, new ZmqPump(), motdelafin, args);
	}

	/**
	 * Starts the proxy.
	 *
	 * @param sync true to read the status in synchronous way, false for asynchronous mode
	 * @return the read status
	 */
	public String start(boolean sync) {
		return command(START, sync);
	}

	/**
	 * Pauses the proxy.
	 * A paused proxy will cease processing messages, causing
	 * them to be queued up and potentially hit the high-water mark on the
	 * frontend or backend socket, causing messages to be dropped, or writing
	 * applications to block.
	 *
	 * @param sync true to read the status in synchronous way, false for asynchronous mode
	 * @return the read status
	 */
	public String pause(boolean sync) {
		return command(PAUSE, sync);
	}

	/**
	 * Stops the proxy.
	 *
	 * @param sync true to read the status in synchronous way, false for asynchronous mode
	 * @return the read status
	 */
	public String stop(boolean sync) {
		return command(STOP, sync);
	}

	/**
	 * Sends a command message to the proxy actor.
	 * Can be useful for programmatic interfaces.
	 * Does not works with commands {@link #CONFIG CONFIG} and {@link #RESTART RESTART}.
	 *
	 * @param command the command to execute.
	 * @param sync    true to read the status in synchronous way, false for asynchronous mode
	 * @return the read status
	 */
	public String command(String command, boolean sync) {
		assert (!command.equals(CONFIG));
		assert (!command.equals(RESTART));
		if (command.equals(STATUS)) {
			return status(sync);
		}
		if (command.equals(EXIT)) {
			return exit(sync);
		}
		// consume the status in the pipe
		String status = recvStatus();

		if (agent.send(command)) {
			// the pipe is refilled
			if (sync) {
				status(true);
			}
		}
		return status;
	}

	/**
	 * Sends a command message to the proxy actor.
	 * Can be useful for programmatic interfaces.
	 * Does not works with commands {@link Command#CONFIG CONFIG} and {@link Command#RESTART RESTART}.
	 *
	 * @param command the command to execute.
	 * @param sync    true to read the status in synchronous way, false for asynchronous mode
	 * @return the read state
	 */
	public State command(Command command, boolean sync) {
		return State.valueOf(command(command.name(), sync));
	}

	/**
	 * Sends a command message to the proxy actor.
	 * Can be useful for programmatic interfaces.
	 * Works only with commands {@link Command#CONFIG CONFIG} and {@link Command#RESTART RESTART}.
	 *
	 * @param command the command to execute.
	 * @param msg     the custom message to transmit.
	 * @param sync    true to read the status in synchronous way, false for asynchronous mode
	 * @return the response message
	 */
	public ZMsg command(Command command, ZMsg msg, boolean sync) {
		if (command == Command.CONFIG) {
			return configure(msg);
		}
		if (command == Command.RESTART) {
			String status = restart(msg);
			msg = new ZMsg();
			msg.add(status);
			return msg;
		}
		return null;
	}

	/**
	 * Configures the proxy.
	 * The distant side has to send back one (1) mandatory response message.
	 *
	 * @param msg the custom message sent as configuration tip
	 * @return the mandatory response message of the configuration.
	 */
	public ZMsg configure(ZMsg msg) {
		msg.addFirst(CONFIG);

		if (agent.send(msg)) {
			// consume the status in the pipe
			recvStatus();

			ZMsg reply = agent.recv();
			assert (reply != null);

			// refill the pipe with status
			agent.send(STATUS);
			return reply;
		}
		return null;
	}

	/**
	 * Restarts the proxy. Stays alive.
	 *
	 * @param hot null to make a cold restart (closing then re-creation of the sockets)
	 *            or a configuration message to perform a configurable hot restart,
	 */
	public String restart(ZMsg hot) {
		ZMsg msg = new ZMsg();
		msg.add(RESTART);

		if (hot == null) {
			msg.add(Boolean.toString(false));
		} else {
			msg.add(Boolean.toString(true));
			// FIXME better way to append 1 message into another ?
			for (int index = 0; index < hot.size(); ++index) {
				ZFrame frame = hot.pop();
				msg.add(frame);
			}
		}

		String status = EXITED;
		if (agent.send(msg)) {
			status = status(false);
		}
		return status;
	}

	/**
	 * Stops the proxy and exits.
	 *
	 * @param sync true to read the status in synchronous way, false for asynchronous mode
	 * @return the read status
	 */
	public String exit(boolean sync) {
		String status = EXITED;
		if (agent.send(EXIT)) {
			if (sync) {
				return await();
			}
			status = status(false);
		}
		return status;
	}

	// Waits for the completion of this proxy, like in old style.
	private String await() {
		String status = status(true);
		while (!Thread.currentThread().isInterrupted()) {
			if (EXITED.equals(status)) {
				break;
			}
			if (!agent.sign()) {
				return EXITED;
			}
			status = status(false);
		}
		return status;
	}

	/**
	 * Inquires for the status of the proxy.
	 * This call is synchronous.
	 */
	public String status() {
		return status(true);
	}

	/**
	 * Inquires for the status of the proxy.
	 *
	 * @param sync true to read the status in synchronous way, false for asynchronous mode.
	 *             If false, you get the last cached status of the proxy
	 */
	public String status(boolean sync) {
		String status = recvStatus();

		if (agent.send(STATUS) && sync) {
			// wait for the response to emulate sync
			status = recvStatus();
			// AND refill a status
			if (!agent.send(STATUS)) {
				// TODO and in case of error? Let's handle it with exception for now
				throw new RuntimeException("Unable to send the status message");
			}
		}
		return status;
	}

	// receives the last known state of the proxy
	private String recvStatus() {
		if (!agent.sign()) {
			return EXITED;
		}
		// receive the status response
		final ZMsg msg = agent.recv();

		if (msg == null) {
			return EXITED;
		}

		String status = msg.popString();
		msg.destroy();
		return status;
	}

	/**
	 * Binary inquiry for the status of the proxy.
	 */
	public boolean isStarted() {
		return started();
	}

	/**
	 * Binary inquiry for the status of the proxy.
	 */
	public boolean started() {
		String status = status(true);
		return STARTED.equals(status);
	}

	// to handle commands in a more java-centric way
	public enum Command {
		START,
		PAUSE,
		STOP,
		RESTART,
		EXIT,
		STATUS,
		CONFIG
	}

	// commands for the control pipe
	private static final String START = Command.START.name();
	private static final String PAUSE = Command.PAUSE.name();
	private static final String STOP = Command.STOP.name();
	private static final String RESTART = Command.RESTART.name();
	private static final String EXIT = Command.EXIT.name();
	private static final String STATUS = Command.STATUS.name();
	private static final String CONFIG = Command.CONFIG.name();

	// to handle states in a more java-centric way
	public enum State {
		ALIVE,
		STARTED,
		PAUSED,
		STOPPED,
		EXITED
	}

	// state responses from the control pipe
	public static final String STARTED = State.STARTED.name();
	public static final String PAUSED = State.PAUSED.name();
	public static final String STOPPED = State.STOPPED.name();
	public static final String EXITED = State.EXITED.name();
	// defines the very first time where no command changing the state has been issued
	public static final String ALIVE = State.ALIVE.name();

	private static final AtomicInteger counter = new AtomicInteger();

	// the endpoint to the distant proxy actor
	private final ZAgent agent;

	/**
	 * Creates a new unnamed proxy.
	 *
	 * @param selector the creator of the selector used for the proxy.
	 * @param creator  the creator of the sockets of the proxy.
	 */
	public ZProxy(SelectorCreator selector, Proxy creator,
				  String motdelafin, Object... args) {
		this(null, null, selector, creator, null, motdelafin, args);
	}

	/**
	 * Creates a new named proxy.
	 *
	 * @param name     the name of the proxy (used in threads naming).
	 * @param selector the creator of the selector used for the proxy.
	 * @param creator  the creator of the sockets of the proxy.
	 */
	public ZProxy(String name, SelectorCreator selector,
				  Proxy creator, String motdelafin, Object... args) {
		this(null, name, selector, creator, null, motdelafin, args);
	}

	/**
	 * Creates a new named proxy.
	 *
	 * @param ctx      the main context used.
	 *                 If null, a new context will be created and closed at the stop of the operation.
	 *                 <b>If not null, it is the responsibility of the call to close it.</b>
	 * @param name     the name of the proxy (used in threads naming).
	 * @param selector the creator of the selector used for the proxy.
	 * @param sockets  the creator of the sockets of the proxy.
	 * @param pump     the pump used for the proxy
	 */
	public ZProxy(ZContext ctx, String name, SelectorCreator selector,
				  Proxy sockets, Pump pump, String motdelafin, Object... args) {
		super();

		// arguments parsing
		if (pump == null) {
			pump = new ZmqPump();
		}
		int count = 1;
		count += args.length;

		Object[] vars = null;
		vars = new Object[count];

		vars[0] = sockets;
		Actor shadow = null;

		// copy the arguments and retrieve the last optional shadow given in input
		for (int index = 0; index < args.length; ++index) {
			Object arg = args[index];
			if (arg instanceof Actor) {
				shadow = (Actor) arg;
			}
			vars[index + 1] = arg;
		}

		// handle the actor
		int id = counter.incrementAndGet();
		Actor actor = new ProxyActor(name, pump, id);
		if (shadow != null) {
			actor = new ZActor.Duo(actor, shadow);
		}

		ZActor zactor = new ZActor(ctx, selector, actor, motdelafin, vars);
		agent = zactor.agent(); // the zactor is also its own agent
	}

	// defines a pump that will flow messages from one socket to another
	public interface Pump {
		/**
		 * Transfers a message from one source to one destination, with an optional capture.
		 *
		 * @param src         the plug of the source socket
		 * @param source      the socket where to receive the message from.
		 * @param capture     the optional sockets where to send the message to. Possibly null.
		 * @param dst         the plug of the destination socket
		 * @param destination the socket where to send the message to.
		 * @return false in case of error or interruption, true if successfully transferred the message
		 */
		boolean flow(Plug src, Socket source, Socket capture,
					 Plug dst, Socket destination);
	}

	// acts in background to proxy messages
	private static final class ProxyActor extends ZActor.SimpleActor {
		// the states container of the proxy
		private static final class State {
			// are we alive ?
			private boolean alive = false;
			// are we started ?
			private boolean started = false;
			// are we paused ?
			private boolean paused = false;

			// controls creation of a new agent if asked of a cold restart
			private boolean restart = false;
			// one-shot configuration for hot restart
			private ZMsg hot;
		}

		// the state of the proxy
		private final State state = new State();

		// used to transfer message from one socket to another
		private final Pump transport;

		// the nice name of the proxy
		private final String name;

		// the provider of the sockets
		private Proxy provider;

		// the sockets creator user arguments
		private Object[] args;

		private Socket frontend;
		private Socket backend;
		private Socket capture;

		// creates a new Proxy actor.
		public ProxyActor(String name, Pump transport, int id) {
			if (name == null) {
				// default basic name
				this.name = String.format("ZProxy-%sd", id);
			} else {
				this.name = name;
			}
			this.transport = transport;
		}

		@Override
		public String premiere(Socket pipe) {
			return name;
		}

		// creates the sockets before the start of the proxy
		@Override
		public List<Socket> createSockets(ZContext ctx, Object[] args) {
			provider = (Proxy) args[0];

			this.args = new Object[args.length - 1];
			System.arraycopy(args, 1, this.args, 0, this.args.length);

			frontend = provider.create(ctx, Plug.FRONT, this.args);
			capture = provider.create(ctx, Plug.CAPTURE, this.args);
			backend = provider.create(ctx, Plug.BACK, this.args);

			assert (frontend != null);
			assert (backend != null);

			return Arrays.asList(frontend, backend);
		}

		@Override
		public void start(Socket pipe, List<Socket> sockets, ZPoller poller) {
			// init the state machine
			state.alive = true;

			ZMsg reply = new ZMsg();
			reply.add(ALIVE);
			reply.send(pipe);
		}

		// Process a control message
		@Override
		public boolean backstage(Socket pipe, ZPoller poller, int events) {
			assert (state.hot == null);

			String cmd = pipe.recvStr();
			// a message has been received from the API
			if (START.equals(cmd)) {
				start(poller);
				return status().send(pipe);
			} else if (STOP.equals(cmd)) {
				stop(poller);
				return status().send(pipe);
			} else if (PAUSE.equals(cmd)) {
				pause(poller, true);
				return status().send(pipe);
			} else if (RESTART.equals(cmd)) {
				String val = pipe.recvStr();
				boolean hot = Boolean.parseBoolean(val);
				return restart(pipe, poller, hot);
			} else if (STATUS.equals(cmd)) {
				return status().send(pipe);
			} else if (CONFIG.equals(cmd)) {
				ZMsg cfg = ZMsg.recvMsg(pipe);
				boolean rc = provider.configure(pipe, cfg, frontend, backend, capture, args);
				cfg.destroy();
				return rc;
			} else if (EXIT.equals(cmd)) {
				// stops the proxy and the agent.
				// the status will be sent at the end of the loop
			} else {
				return provider.custom(pipe, cmd, frontend, backend, capture, args);
			}
			return false;
		}

		// returns the status
		private ZMsg status() {
			ZMsg reply = new ZMsg();
			if (!state.alive) {
				reply.add(EXITED);
				return reply;
			}
			if (state.started) {
				if (state.paused) {
					reply.add(PAUSED);
				} else {
					reply.add(STARTED);
				}
			} else {
				reply.add(STOPPED);
			}
			return reply;
		}

		// starts the proxy sockets
		private boolean start(ZPoller poller) {
			if (!state.started) {
				state.started = true;

				provider.configure(frontend, Plug.FRONT, args);
				provider.configure(backend, Plug.BACK, args);
				provider.configure(capture, Plug.CAPTURE, args);
			}
			if (!state.paused) {
				pause(poller, false);
			}
			return true;
		}

		// pauses the proxy sockets
		private boolean pause(ZPoller poller, boolean pause) {
			state.paused = pause;
			if (pause) {
				poller.unregister(frontend);
				poller.unregister(backend);
				// TODO why not a mechanism for eventually flushing the sockets during the pause?
			} else {
				poller.register(frontend, ZPoller.POLLIN);
				poller.register(backend, ZPoller.POLLIN);
				//  Now Wait also until there are either requests or replies to process.
			}
			return true;
		}

		private boolean stop(ZPoller poller) {
			// restart the actor in stopped state
			state.started = false;
			// close connections
			state.restart = true;
			return true;
		}

		// handles the restart command in both modes
		private boolean restart(Socket pipe, ZPoller poller, boolean hot) {
			if (hot) {
				assert (provider != null);
				state.hot = ZMsg.recvMsg(pipe);
				state.restart = true;
				// continue with the same agent
				return true;
			} else {
				state.restart = true;
				// stop the loop and restart a new agent
				// with the same started state
				// the next loop will refill the updated status
				return false;
			}
		}

		@Override
		public long looping(Socket pipe, ZPoller poller) {
			state.hot = null;
			return super.looping(pipe, poller);
		}

		// a message has been received for the proxy to process
		@Override
		public boolean stage(Socket socket, Socket pipe, ZPoller poller,
							 int events) {
			if (socket == frontend) {
				//  Process a request.
				return transport.flow(
						Plug.FRONT, frontend,
						capture,
						Plug.BACK, backend);
			}
			if (socket == backend) {
				//  Process a reply.
				return transport.flow(
						Plug.BACK, backend,
						capture,
						Plug.FRONT, frontend);
			}
			return false;
		}

		@Override
		public boolean looped(Socket pipe, ZPoller poller) {
			if (state.restart && state.hot != null) {
				// caught the hot restart
				ZMsg cfg = state.hot;
				state.hot = null;
				state.restart = false;

				boolean cold;
				ZMsg dup = cfg.duplicate();
				cold = provider.restart(dup, frontend, Plug.FRONT, this.args);
				dup.destroy();
				dup = cfg.duplicate();
				cold |= provider.restart(dup, backend, Plug.BACK, this.args);
				dup.destroy();
				dup = cfg.duplicate();
				cold |= provider.restart(dup, capture, Plug.CAPTURE, this.args);
				dup.destroy();
				cfg.destroy();

				// we perform a cold restart if the provider says so
				state.restart = cold;
			}
			return true;
		}

		// called in the proxy thread when it stopped.
		@Override
		public boolean destroyed(Socket pipe, ZPoller poller) {
			if (capture != null) {
				capture.close();
			}
			if (!state.restart) {
				state.alive = false;
				status().send(pipe);
			}
			return state.restart;
		}
	}

	/**
	 * A pump that reads a message as a whole before transmitting it.
	 * It offers a way to transform messages for capture and destination.
	 */
	public static class ZPump implements Pump {
		private static final Identity IDENTITY = new Identity();

		// the messages transformer
		private final Transformer transformer;

		// transforms one message into another
		public interface Transformer {
			/**
			 * Transforms a ZMsg into another ZMsg.
			 * Please note that this will be used during the message transfer,
			 * so lengthy operations will have a cost on performances by definition.
			 * If you return back another message than the one given in input, then this one has to be destroyed by you.
			 *
			 * @param msg the message to transform
			 * @param src the source plug
			 * @param dst the destination plug
			 * @return the transformed message
			 */
			ZMsg transform(ZMsg msg, Plug src, Plug dst);
		}

		private static class Identity implements Transformer {
			@Override
			public ZMsg transform(ZMsg msg, Plug src, Plug dst) {
				return msg;
			}
		}

		public ZPump() {
			this(null);
		}

		public ZPump(Transformer transformer) {
			super();
			this.transformer = transformer == null ? IDENTITY : transformer;
		}

		@Override
		public boolean flow(Plug splug, Socket source, Socket capture,
							Plug dplug, Socket destination) {
			boolean success = false;

			// we read the whole message
			ZMsg msg = ZMsg.recvMsg(source);

			if (msg == null) {
				return false;
			}

			if (capture != null) {
				//  Copy transformed message to capture socket if any message
				// TODO what if the transformer modifies or destroys the original message ?
				ZMsg cpt = transformer.transform(msg, splug, Plug.CAPTURE);

//                boolean destroy = !msg.equals(cpt); // TODO ?? which one
				boolean destroy = msg != cpt;
				success = cpt.send(capture, destroy);
				if (!success) {
					// not successful, but we can still try to send it to the destination
				}
			}

			ZMsg dst = transformer.transform(msg, splug, dplug);
			// we send the whole transformed message
			success = dst.send(destination);

			// finished
			msg.destroy();

			return success;
		}
	}

	/**
	 * A specialized transport for better transmission purposes
	 * that will send each packets individually instead of the whole message.
	 */
	private static final class ZmqPump implements Pump {
		// transfers each message as a whole by sending each packet received to the capture socket
		@Override
		public boolean flow(Plug splug, Socket source, Socket capture,
							Plug dplug, Socket destination) {
			boolean rc;

			SocketBase src = source.base();
			SocketBase dst = destination.base();
			SocketBase cpt = capture == null ? null : capture.base();

			// we transfer the whole message
			while (true) {
				// we read the packet
				Msg msg = src.recv(0);

				if (msg == null) {
					return false;
				}

				long more = src.getSocketOpt(zmq.ZMQ.ZMQ_RCVMORE);

				if (more < 0) {
					return false;
				}

				//  Copy message to capture socket if any packet
				if (cpt != null) {
					Msg ctrl = new Msg(msg);
					rc = cpt.send(ctrl, more > 0 ? zmq.ZMQ.ZMQ_SNDMORE : 0);
					if (!rc) {
						// not successful, but we can still try to send it to the destination
					}
				}

				// we send the packet
				rc = dst.send(msg, more > 0 ? zmq.ZMQ.ZMQ_SNDMORE : 0);

				if (!rc) {
					return false;
				}
				if (more == 0) {
					break;
				}
			}
			return true;
		}
	}
}
