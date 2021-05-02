mother(alice,bill).
son(X,Y):-mother(Y,X),boy(X).

member(X,[X|T])
member(X,[H|T]) :- member(X,T)

append([],L,L)
append([X|A],B,[X|C]) :- append(A,B,C)

trace=1.
member(a,[a,b,c])?
member(X,[a,b,c])?

append([a,b],[c,d],X)?
append([a,b],Y,[a,b,c,d])?
append(X,Y,[a,b,c])?

quit.
