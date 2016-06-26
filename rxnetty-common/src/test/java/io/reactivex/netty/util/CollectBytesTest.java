package io.reactivex.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.util.CollectBytes.TooMuchDataException;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectBytesTest {
    @Test
    public void testCollectOverEmptyObservable() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.<ByteBuf>empty()
            .compose(CollectBytes.all())
            .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValue(Unpooled.buffer());
    }

    @Test
    public void testCollectSingleEvent() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(Unpooled.copiedBuffer("test", Charset.forName("UTF-8")))
                .compose(CollectBytes.all())
                .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValues(Unpooled.copiedBuffer("test", Charset.forName("UTF-8")));
    }

    @Test
    public void testCollectManyEvents() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(
                    Unpooled.copiedBuffer("t", Charset.forName("UTF-8")),
                    Unpooled.copiedBuffer("e", Charset.forName("UTF-8")),
                    Unpooled.copiedBuffer("s", Charset.forName("UTF-8")),
                    Unpooled.copiedBuffer("t", Charset.forName("UTF-8"))
                )
                .compose(CollectBytes.all())
                .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValues(Unpooled.copiedBuffer("test", Charset.forName("UTF-8")));
    }

    @Test
    public void testWithLimitEqualToBytes() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("e", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("s", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8"))
        )
                .compose(CollectBytes.upTo(4))
                .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValues(Unpooled.copiedBuffer("test", Charset.forName("UTF-8")));
    }

    @Test
    public void testWithLimitGreaterThanBytes() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("e", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("s", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8"))
        )
                .compose(CollectBytes.upTo(5))
                .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValues(Unpooled.copiedBuffer("test", Charset.forName("UTF-8")));
    }

    @Test
    public void testCollectWithLimitSmallerThanBytes() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("e", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("s", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8"))
        )
                .compose(CollectBytes.upTo(2))
                .subscribe(t);

        t.assertError(TooMuchDataException.class);
        t.assertNotCompleted();
        t.assertNoValues();
    }

    @Test
    public void testCollectWithLimitSmallerThanBytesWrapsTooMuchData() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        Observable.just(
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("e", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("s", Charset.forName("UTF-8")),
                Unpooled.copiedBuffer("t", Charset.forName("UTF-8"))
        )
                .compose(CollectBytes.upTo(2))
                .subscribe(t);

        Assert.assertEquals(TooMuchDataException.class, t.getOnErrorEvents().get(0).getClass());
        Assert.assertEquals(getByteBuf("te"), ((OnErrorThrowable.OnNextValue) t.getOnErrorEvents().get(0).getCause()).getValue());
    }

    @Test
    public void testReturnSingleEventWithMoreBytesThanMax() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        toByteBufObservable("test")
                .compose(CollectBytes.upTo(0))
                .subscribe(t);

        Assert.assertEquals(TooMuchDataException.class, t.getOnErrorEvents().get(0).getClass());
        Assert.assertEquals(Unpooled.EMPTY_BUFFER, ((OnErrorThrowable.OnNextValue) t.getOnErrorEvents().get(0).getCause()).getValue());
    }

    @Test
    public void testReturnMultipleEvents() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        toByteBufObservable("1", "2")
                .compose(CollectBytes.upTo(5))
                .subscribe(t);

        t.assertNoErrors();
        t.assertCompleted();
        t.assertValues(getByteBufs("12"));
    }

    @Test
    public void testReturnEventsOnLimitBoundary() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        toByteBufObservable("12", "34", "56")
                .compose(CollectBytes.upTo(4))
                .subscribe(t);

        Assert.assertEquals(TooMuchDataException.class, t.getOnErrorEvents().get(0).getClass());
        Assert.assertEquals(getByteBuf("1234"), ((OnErrorThrowable.OnNextValue) t.getOnErrorEvents().get(0).getCause()).getValue());
    }

    @Test
    public void testReturnMultipleEventsEndingWhenOverMaxBytes() throws Exception {
        TestSubscriber<ByteBuf> t = new TestSubscriber<>();
        toByteBufObservable("first", "second", "third")
                .compose(CollectBytes.upTo(7))
                .subscribe(t);

        Assert.assertEquals(TooMuchDataException.class, t.getOnErrorEvents().get(0).getClass());
        Assert.assertEquals(getByteBuf("first"), ((OnErrorThrowable.OnNextValue) t.getOnErrorEvents().get(0).getCause()).getValue());
    }

    @Test
    public void testUnsubscribeFromUpstream() throws Exception {
        final List<ByteBuf> emittedBufs = new ArrayList<>();

        toByteBufObservable("first", "second", "third")
                .doOnNext(new Action1<ByteBuf>() {
                    @Override
                    public void call(ByteBuf byteBuf) {
                        emittedBufs.add(byteBuf);
                    }
                })
                .compose(CollectBytes.upTo(7))
                .subscribe(new TestSubscriber<>());

        Assert.assertEquals(Arrays.asList(getByteBufs("first", "second")), emittedBufs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionOnNegativeMaxBytes() throws Exception {
        CollectBytes.upTo(-1);
    }

    private Observable<ByteBuf> toByteBufObservable(String... values) {
        return Observable.from(values)
                .map(new Func1<String, ByteBuf>() {
                    @Override
                    public ByteBuf call(String s) {
                        return getByteBuf(s);
                    }
                });
    }

    private ByteBuf getBytes(int length, int value) {
        ByteBuf buffer = Unpooled.buffer(length, length);
        for (int i = 0; i < length; ++i) {
            buffer.writeByte(value);
        }
        return buffer;
    }

    private ByteBuf getByteBuf(String s) {
        return Unpooled.copiedBuffer(s, Charset.forName("UTF-8"));
    }

    private ByteBuf[] getByteBufs(String... s) {
        ByteBuf[] bufs = new ByteBuf[s.length];
        for (int i = 0; i < s.length; ++i) {
            bufs[i] = getByteBuf(s[i]);
        }
        return bufs;
    }
}