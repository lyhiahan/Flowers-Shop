const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/profile-images.js'),'utf8');

function fixture() {
    const inputs=['avatar','cover'].map(kind => ({id:kind+'FileInput',dataset:{profileImage:kind+'Preview'},files:[],value:'',listeners:{},
        addEventListener(name,fn){this.listeners[name]=fn;},click(){this.clicked=true;}}));
    const buttons=inputs.map(input => ({dataset:{profileImageTrigger:input.id},listeners:{},addEventListener(name,fn){this.listeners[name]=fn;}}));
    const avatar={src:'/old-avatar.png',getAttribute(){return '/old-avatar.png';}};
    const cover={src:'/old-cover.png',getAttribute(){return '/old-cover.png';}};
    const error={hidden:true,textContent:''}, filename={textContent:''}, revoked=[], listeners={};
    const form={querySelectorAll(selector){return selector.startsWith('input') ? inputs : buttons;}};
    const elements={profileForm:form,avatarPreview:avatar,coverPreview:cover,profileImageError:error,avatarFileName:filename};
    for (const input of inputs) elements[input.id]=input;
    vm.runInNewContext(source,{document:{getElementById(id){return elements[id];}},
        window:{addEventListener(name,fn){listeners[name]=fn;}},
        URL:{createObjectURL(file){return 'blob:'+file.name;},revokeObjectURL(url){revoked.push(url);}}});
    const select=(index,file) => {inputs[index].files=file ? [file] : [];inputs[index].listeners.change();};
    return {inputs,buttons,avatar,cover,error,filename,revoked,listeners,select};
}
const image=name => ({name,type:'image/png',size:1024});

test('visible photo buttons open their corresponding file pickers',() => {
    const ui=fixture();
    ui.buttons.forEach(button=>button.listeners.click());
    assert.equal(ui.inputs[0].clicked,true);assert.equal(ui.inputs[1].clicked,true);
});
test('avatar and cover previews update independently before saving',() => {
    const ui=fixture();
    ui.select(0,image('avatar.png'));
    assert.equal(ui.avatar.src,'blob:avatar.png');assert.equal(ui.cover.src,'/old-cover.png');
    assert.equal(ui.filename.textContent,'avatar.png');
    ui.select(1,image('cover.png'));
    assert.equal(ui.cover.src,'blob:cover.png');assert.equal(ui.avatar.src,'blob:avatar.png');
    assert.equal(ui.error.hidden,true);
});
test('unsupported files and oversized images are rejected without replacing saved photos',() => {
    for (const file of [{name:'bad.svg',type:'image/svg+xml',size:20}, {...image('large.png'),size:11*1024*1024}]) {
        const ui=fixture();ui.select(0,file);
        assert.equal(ui.inputs[0].value,'');assert.equal(ui.avatar.src,'/old-avatar.png');
        assert.equal(ui.error.hidden,false);assert.notEqual(ui.error.textContent,'');
    }
});
test('replacing previews releases old URLs and an invalid replacement restores the saved image',() => {
    const ui=fixture();
    ui.select(0,image('first.png'));ui.select(0,image('second.png'));
    assert.deepEqual(ui.revoked,['blob:first.png']);
    ui.select(0,{name:'bad.txt',type:'text/plain',size:10});
    assert.equal(ui.avatar.src,'/old-avatar.png');
    assert.deepEqual(ui.revoked,['blob:first.png','blob:second.png']);
});
test('cancelled selection leaves the existing preview and other file intact',() => {
    const ui=fixture();ui.select(0,image('avatar.png'));ui.select(1,null);
    assert.equal(ui.avatar.src,'blob:avatar.png');assert.equal(ui.cover.src,'/old-cover.png');
});
test('leaving the page releases both selected image URLs',() => {
    const ui=fixture();ui.select(0,image('avatar.png'));ui.select(1,image('cover.png'));ui.listeners.pagehide();
    assert.deepEqual(ui.revoked,['blob:avatar.png','blob:cover.png']);
});
