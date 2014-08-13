package io.reactivex.netty.channel;


import rx.Observable;

public interface Handler<IN, OUT> {

    Observable<Void> handle(IN input, OUT output);

}
