import AddComment from './AddComment';

async function run() {
    const  addComment = new AddComment();
    try {
        await addComment.addComment("Some Comment");
    }catch (e) {
        console.log(e);
    }

}

run().catch(e => console.log(e));