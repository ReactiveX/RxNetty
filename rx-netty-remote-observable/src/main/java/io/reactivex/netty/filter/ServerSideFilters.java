package io.reactivex.netty.filter;

import java.util.Map;

import rx.functions.Func1;

public class ServerSideFilters {

	private ServerSideFilters(){}
	
	public static <T> Func1<Map<String, String>, Func1<T, Boolean>> noFiltering(){
		return new Func1<Map<String,String>, Func1<T,Boolean>>(){
			@Override
			public Func1<T, Boolean> call(Map<String, String> t1) {
				return new Func1<T,Boolean>(){
					@Override
					public Boolean call(T t1) {
						return true;
					}
				};
			}
		};
	}
	
	public static Func1<Map<String, String>, Func1<Integer, Boolean>> oddsAndEvens(){
		return new Func1<Map<String,String>, Func1<Integer,Boolean>>(){
			@Override
			public Func1<Integer, Boolean> call(final Map<String, String> params) {
				return new Func1<Integer,Boolean>(){
					@Override
					public Boolean call(Integer t1) {
						if (params == null || params.isEmpty() || 
								!params.containsKey("type")){
							return true; // no filtering
						}else{
							String type = params.get("type");
							if ("even".equals(type)){
								return (t1 % 2 == 0);
							}else if ("odd".equals(type)){
								return (t1 % 2 != 0);
							}else{
								throw new RuntimeException("Unsupported filter param 'type' for oddsAndEvens: "+type);
							}
						}
					}
				};
			}
		};
	}
}
