import AddComment from './AddComment';
import * as core from '@actions/core';
async function run() {
    const  addComment = new AddComment();
    try {
        await addComment.addComment("Some Comment");
    }catch (error) {
        core.setFailed(error.message);
    }

}

run().catch(e => core.setFailed(e));