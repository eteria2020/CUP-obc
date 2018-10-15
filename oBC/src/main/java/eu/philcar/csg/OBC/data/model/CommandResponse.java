package eu.philcar.csg.OBC.data.model;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 16/02/2018.
 */
public class CommandResponse extends BaseResponse {
	private int id;
	private String command;
	private int intarg1;
	private int intarg2;
	private String txtarg1;
	private String txtarg2;
	private long queued;
	private int ttl;
	private String payload;

	public CommandResponse(int id, String command, int intarg1, int intarg2, String txtarg1, String txtarg2, long queued, int ttl, String payload) {
		this.id = id;
		this.command = command;
		this.intarg1 = intarg1;
		this.intarg2 = intarg2;
		this.txtarg1 = txtarg1;
		this.txtarg2 = txtarg2;
		this.queued = queued;
		this.ttl = ttl;
		this.payload = payload;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public int getIntarg1() {
		return intarg1;
	}

	public void setIntarg1(int intarg1) {
		this.intarg1 = intarg1;
	}

	public int getIntarg2() {
		return intarg2;
	}

	public void setIntarg2(int intarg2) {
		this.intarg2 = intarg2;
	}

	public String getTxtarg1() {
		return txtarg1;
	}

	public void setTxtarg1(String txtarg1) {
		this.txtarg1 = txtarg1;
	}

	public String getTxtarg2() {
		return txtarg2;
	}

	public void setTxtarg2(String txtarg2) {
		this.txtarg2 = txtarg2;
	}

	public long getQueued() {
		return queued;
	}

	public void setQueued(long queued) {
		this.queued = queued;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
}
//[{"id":3916694,"command":"SET_ENGINE","intarg1":0,"intarg2":0,"txtarg1":null,"txtarg2":null,"queued":"1518793119","ttl":180,"payload":null},{"id":3916696,"command":"SET_DOORS","intarg1":1,"intarg2":0,"txtarg1":null,"txtarg2":null,"queued":"1518793127","ttl":180,"payload":null}]