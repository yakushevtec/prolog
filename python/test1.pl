child(X) :- boy(X).
child(X) :- girl(X).
girl(alice).
boy(alex).
trace=1.
child(Q)?
quit.
