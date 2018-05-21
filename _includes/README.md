Varviz
======
One of the main challenges of debugging is to understand why the program fails for certain inputs but succeeds for others.
This becomes especially difficult if the fault is caused by an interaction of multiple inputs.
To debug such interaction faults, it is necessary to understand the individual effect of the input, how these inputs interact and how these interactions cause the fault.
The differences between two execution traces can explain why one input behaves differently than the other.
We propose to compare execution traces of all input options to derive explanations of the behavior of all options and interactions among them.
To make the relevant information stand out, we represent them as variational traces that concisely represents control-flow and data-flow differences among multiple concrete traces.
While variational traces can be obtained from brute-force execution of all relevant inputs, we use variational execution to scale the generation of variational traces to the exponential space of possible inputs. 
We further provide an Eclipse plugin Varviz that enables users to use variational traces for debugging and navigation.
In a user study, we show that users of variational traces are more than twice as fast to finish debugging tasks than users of the standard Eclipse debugger.
We further show that variational traces can be scaled to programs with many options.

<a href="/resources/varviz/ICSEPoster.png"><img alt="Poster" src="/resources/varviz/ICSEPoster.png" width="800"/></a>

## Download

[WIN-64 Packaged](https://cmu.box.com/s/5zov85s94l7yiu4fkkilidcgydcesaeg)

[MAC Packaged](https://cmu.box.com/s/5fmcfgx3jjciao3edl74e4evwf0vsz69)

