package eu.daiad.web.model.error;

public enum SharedErrorCode implements ErrorCode {
	UNKNOWN,
	NOT_IMPLEMENTED,
	PARSE_ERROR,
	AUTHENTICATION,
	AUTHENTICATION_NO_CREDENTIALS,
	AUTHENTICATION_USERNAME,
	AUTHORIZATION,
	AUTHORIZATION_ANONYMOUS_SESSION,
	AUTHORIZATION_MISSING_ROLE,
	RESOURCE_NOT_FOUND,
	METHOD_NOT_SUPPORTED,
	FILE_DOES_NOT_EXIST,
	INVALID_TIME_ZONE,
	TIMEZONE_NOT_FOUND,
	DIR_CREATION_FAILED,
	INVALID_SRID;

	@Override
	public String getMessageKey() {
		return (this.getClass().getSimpleName() + '.' + this.name());
	}
}
