package gedi.remote;

import java.io.IOException;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class StatusMessage implements BinarySerializable {

	private String status;
	
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(status.length());
		out.putAsciiChars(status);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		status = in.getAsciiChars(in.getCInt());
	}

}
