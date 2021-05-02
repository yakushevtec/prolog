length([],0)
length([H|T],N) :- length(T,Nt), N is Nt+1

trace=1.

length([a,b,c],X)?
length([a,b,c],3)?

childOf(X,Y) :- parent(Y,X)
parent(chris,jon)
parent(maryann,jon)
childOf(A,B)?

childOfCut(X,Y) :- parent(Y,X),cut
childOfCut(A,B)?

quit.
