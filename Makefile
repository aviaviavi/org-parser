
.PHONY: build test testjs testjar clean

build: buildjs.sh buildjar.sh
	bash buildjar.sh
	bash buildjs.sh
# Order is important because one command deletes build artifacts of the other.
# Also, not sure if this would stop correctly on error:
# echo $^ | xargs -n 1 bash

test: testjs testjar
# Test by executing parser on test file and expect at least one line of output (grep .)

testjs: testjs.sh
	bash testjs.sh | grep .

testjar: testjar.sh
	bash testjar.sh | grep .


# Filenames of :tangle files must be hardcoded :(
buildjs.sh buildjar.sh testjs.sh testjar.sh: README.org
	emacs --batch -l org $< -f org-babel-tangle

clean:
	$(RM) build*.sh
	$(RM) test*.sh
