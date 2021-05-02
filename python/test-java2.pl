mother(alice,bill).
son(X,Y):-mother(Y,X),boy(X).

member(X,[X|T])
member(X,[H|T]) :- member(X,T)

append([],L,L)
append([X|A],B,[X|C]) :- append(A,B,C)
