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

package zmq;

public class Dealer extends SocketBase {
	public static class DealerSession extends SessionBase {
		public DealerSession(IOThread ioThread, boolean connect,
							 SocketBase socket, final Options options,
							 final Address addr) {
			super(ioThread, connect, socket, options, addr);
		}
	}

	//  Messages are fair-queued from inbound pipes. And load-balanced to
	//  the outbound pipes.
	private final FQ fq;
	private final LB lb;

	//  Have we prefetched a message.
	private boolean prefetched;

	private Msg prefetchedMsg;

	//  Holds the prefetched message.
	public Dealer(Ctx parent, int tid, int sid) {
		super(parent, tid, sid);

		prefetched = false;
		options.type = ZMQ.ZMQ_DEALER;

		fq = new FQ();
		lb = new LB();
		//  TODO: Uncomment the following line when DEALER will become true DEALER
		//  rather than generic dealer socket.
		//  If the socket is closing we can drop all the outbound requests. There'll
		//  be noone to receive the replies anyway.
		//  options.delayOnClose = false;

		options.recvIdentity = true;
	}

	@Override
	protected void xattachPipe(Pipe pipe, boolean icanhasall) {
		assert (pipe != null);
		fq.attach(pipe);
		lb.attach(pipe);
	}

	@Override
	protected boolean xsend(Msg msg) {
		return lb.send(msg, errno);
	}

	@Override
	protected Msg xrecv() {
		return xxrecv();
	}

	private Msg xxrecv() {
		Msg msg = null;
		//  If there is a prefetched message, return it.
		if (prefetched) {
			msg = prefetchedMsg;
			prefetched = false;
			prefetchedMsg = null;
			return msg;
		}

		//  DEALER socket doesn't use identities. We can safely drop it and
		while (true) {
			msg = fq.recv(errno);
			if (msg == null) {
				return null;
			}
			if ((msg.flags() & Msg.IDENTITY) == 0) {
				break;
			}
		}
		return msg;
	}

	@Override
	protected boolean xhasIn() {
		//  We may already have a message pre-fetched.
		if (prefetched) {
			return true;
		}

		//  Try to read the next message to the pre-fetch buffer.
		prefetchedMsg = xxrecv();
		if (prefetchedMsg == null) {
			return false;
		}
		prefetched = true;
		return true;
	}

	@Override
	protected boolean xhasOut() {
		return lb.hasOut();
	}

	@Override
	protected void xreadActivated(Pipe pipe) {
		fq.activated(pipe);
	}

	@Override
	protected void xwriteActivated(Pipe pipe) {
		lb.activated(pipe);
	}

	@Override
	protected void xpipeTerminated(Pipe pipe) {
		fq.terminated(pipe);
		lb.terminated(pipe);
	}
}
