
# Mac instructions:

## Install SuperCollider

https://supercollider.github.io/

## Install my custom classes somewhere:

In a terminal:

```
git clone https://github.com/yeeking/myksupercollider.git
```

should create a folder called myksupercollider with a sub-folder called 
classes

```
cp -r myksupercollider/classes  ~/Library/Application\ Support/SuperCollider/Extensions
```

## Run the improviser


* Set your mac system audio device to the one you want to use for 
SuperCollider

* Plug a mic into input one of your audio card

* Run the SuperCollder system you downloaded 

* Open the file 20150926_martin_speake_at_wellcome_trust_algorithm.sc

* Double click next to the first open bracket to select the first block of 
code

* Execute! 

You should see text in the SuperCollider post window when you sing or play 
notes into the mic like this:

```
note_0.6
note_0.4
note_1.2
```

It will send MIDI out of the first channel on the first MIDI interface it 
sees
