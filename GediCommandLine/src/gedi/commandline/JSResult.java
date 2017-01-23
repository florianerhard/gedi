package gedi.commandline;

public class JSResult {

	private Object result;
	private Throwable exception;
	private boolean canceled = false;
	
	public JSResult(Object result,Throwable exception) {
		this.exception = exception;
		this.result = result;
	}

	public JSResult(boolean canceled) {
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
