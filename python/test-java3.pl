length([],0)
length([H|T],N) :- length(T,Nt), N is Nt+1

childOf(X,Y) :- parent(Y,X)
parent(chris,jon)
parent(maryann,jon)

childOfCut(X,Y) :- parent(Y,X),cut
