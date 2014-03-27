package io.reactivex.netty;

import com.netflix.numerus.NumerusRollingNumberEvent;

public enum RemoteRxNumerusEvent implements NumerusRollingNumberEvent {
	
	BOOTSTRAP(1),NEXT(1),ERROR(1),COMPLETED(1),SUBSCRIBE(1),UNSUBSCRIBE(1),
	CONNECTION_COUNT(1);

	private final int type;

	RemoteRxNumerusEvent(int type) {
        this.type = type;
    }
	
	@Override
	public boolean isCounter() {
        return type == 1;
    }
	@Override
    public boolean isMaxUpdater() {
        return type == 2;
    }
	
	@Override
	public NumerusRollingNumberEvent[] getValues() {
        return values();
	}

}
