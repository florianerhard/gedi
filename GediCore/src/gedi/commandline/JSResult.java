/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.commandline;

public class JSResult {

	private Object result;
	private Throwable exception;
	private boolean canceled = false;
	private String cmd;
	
	public JSResult(String cmd,Object result,Throwable exception) {
		this.cmd = cmd;
		this.exception = exception;
		this.result = result;
	}

	public JSResult(String cmd, boolean canceled) {
		this.cmd = cmd;
		this.canceled = canceled;
	}

	public Throwable getException() {
		return exception;
	}

	public Object getResult() {
		return result;
	}

	public boolean isCanceled() {
		return canceled;
	}
	
	public String getCommand() {
		return cmd;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (canceled ? 1231 : 1237);
		result = prime * result
				+ ((exception == null) ? 0 : exception.hashCode());
		result = prime * result
				+ ((this.result == null) ? 0 : this.result.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSResult other = (JSResult) obj;
		if (canceled != other.canceled)
			return false;
		if (exception == null) {
			if (other.exception != null)
				return false;
		} else if (!exception.equals(other.exception))
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JSResult [result=" + result + ", exception=" + exception
				+ ", canceled=" + canceled + "]";
	}
	
	
	
	
}
